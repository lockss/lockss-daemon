'use strict';

// Bounds how long a single status/finished/errors request can stay
// outstanding. Without this, a slow or stalled request could block the
// next poll from ever being scheduled (see the overlap issue below).
const STATUS_FETCH_TIMEOUT_MS = 10000;

// fetch() with a hard timeout: rejects if the request is still outstanding
// after STATUS_FETCH_TIMEOUT_MS, aborting it so it doesn't linger.
function fetchJsonWithTimeout(url) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), STATUS_FETCH_TIMEOUT_MS);

    return fetch(url, { signal: controller.signal })
        .then(response => response.json())
        .finally(() => clearTimeout(timeoutId));
}

function toggleElement(elem) {
    if (elem === null) {
        return;
    }
    if (elem.style.display === "none") {
        elem.style.display = "block";
    } else {
        elem.style.display = "none";
    }
}

// Append a received page (of finished statuses, or of error/warning
// messages) to the full list, ensuring that duplicate responses are
// handled correctly
function addIncrementalPage(list, page, index) {
    // Avoid copy if not truncating array
    if (list.length == index) {
        return list.concat(page);
    } else {
        return list.slice(0, index).concat(page);
    }
}

class AuMigrationStatus extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      running: false,
      fetchError: true,
      statusList: [ "Loading status" ],
      delay: 1000,
      finishedPageSize: 1,
      finishedCount: 0,
      finishedData: [],
      errorsCount: 0,
      errorsData: [],
      startTime: -1,
    };
  }

  StatusList() {
    if (this.state.statusList === undefined) {
      return null;
    }
    return (
        <div className="stats-div">{this.state.statusList.map((msg, index) => <div key={index}>{msg}<br /></div>)}</div>
    )
  }

  InstrumentList() {
    if (this.state.instrumentList === undefined) {
      return null;
    }
    return (
        <div id="instrumentation" className="stats-div"><font size="2">{this.state.instrumentList.map((msg, index) => <div key={index}>{msg}<br /></div>)}</font></div>
    )
  }

  ActiveList() {
    if (this.state.activeList === undefined) {
      return null;
    }
    return (
        <div className="stats-div">
        {this.state.activeList.length} Tasks Active: <ul>{this.state.activeList.map((msg, index) =>  <li key={index}>{msg}</li>)}</ul></div>
    )
  }

  FinishedList() {
    if (this.state.finishedData === undefined ||
        this.state.finishedData.length == 0) {
      return null;
    }
    return (
        <div className="stats-div">
        {this.state.finishedData.length} Tasks Finished:
        <div className={"finished"} id={"finishedList"}>
        <ul>{this.state.finishedData.map((msg, index) => <li key={index}>{msg}</li>)}</ul>
        </div>
        </div>
    )
  }

  ErrorList() {
    if (this.state.errorsData === undefined ||
        this.state.errorsData.length == 0) {
      return null;
    }
    return (
        <div className="stats-div">
        {this.state.errorsData.length} Errors and Warnings:
        <div className={"errors"}>
        <ul>{this.state.errorsData.map((msg, index) =>  <li key={index}>{msg}</li>)}</ul>
        </div>
        </div>
    )
  }

  componentDidMount() {
    this.mounted = true;
    this.__loadStatus();
    this.disableSomeButtons();
  }

  componentDidUpdate(prevProps, prevState) {
    if (this.state.wasAtBottom) {
      this.__scrollBottom();
    }

    // FIXME: Replace with a jQuery solution?
    if (prevState.running != this.state.running) {
      this.disableSomeButtons();
    }
  }

  disableSomeButtons() {
    console.log("disableSomeButtons: " + this.state.running);
    for (const e of document.querySelectorAll("input[type='submit'],input[type='button'],button[type='submit']")) {
      if (e.value == "Abort") {
        this.disableIfRunning = false;
      } else {
        this.disableIfRunning = true;
      }
      if (this.state.running == this.disableIfRunning) {
        e.setAttribute("disabled", "disabled");
      } else {
        e.removeAttribute("disabled");
      }
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    clearTimeout(this.timeout);
  }

  __scrollBottom = () => {
    const e = document.getElementById("finishedList");
    if (e != null) {
      e.scrollTo({
        top: e.scrollHeight,
        behavior: 'smooth',
      })
    }
  }

  updateStateAfterFetch = (result, prevStartTime) => {
    const e = document.getElementById("finishedList");
    const wasAtBottom =
          (e == null) ||
          ((e != null) &&
           ((e.scrollHeight <= e.clientHeight) ||
            (e.scrollHeight - e.clientHeight) <= e.scrollTop + 5));

    const startTimeChanged = prevStartTime != result.start_time;

    // Resolves once every page this response implies is still needed has
    // settled (successfully or not), so the caller can wait for the whole
    // round trip -- not just this first response -- before scheduling the
    // next poll. Without that, a slow page fetch here would not stop the
    // next status poll from starting, and outstanding requests would pile up.
    return new Promise((resolveRoundTrip) => {
      this.setState((prevState) => ({
        running: result.running,
        fetchError: false,
        statusList: result.status_list,
        instrumentList: result.instrument_list,
        activeList: result.active_list,
        finishedCount: result.finished_count,
        errorsCount: result.errors_count,
        delay: result.running ? 1000 : 5000,
        startTime: result.start_time,
        wasAtBottom: wasAtBottom,
        finishedData: startTimeChanged ? [] : prevState.finishedData,
        errorsData: startTimeChanged ? [] : prevState.errorsData,
      }), () => {
        const pagesPending = [];

        if (this.state.finishedCount != this.state.finishedData.length) {
          pagesPending.push(
            fetchJsonWithTimeout("/MigrateContent?reqfreq=high&output=json&status=finished" +
                  "&index=" + this.state.finishedData.length +
                  "&size=" + (this.state.finishedCount - this.state.finishedData.length))
              .then(
                (result) => {
                  this.setState((prevState) => ({
                    finishedData: addIncrementalPage(prevState.finishedData,
                                                     result.finished_page,
                                                     result.finished_index),
                  }));
                },
                (error) => {
                  console.error("Could not fetch finished AU page: " + error);
                }
              )
          );
        }
        if (this.state.errorsCount != this.state.errorsData.length) {
          pagesPending.push(
            fetchJsonWithTimeout("/MigrateContent?reqfreq=high&output=json&status=errors" +
                  "&index=" + this.state.errorsData.length +
                  "&size=" + (this.state.errorsCount - this.state.errorsData.length))
              .then(
                (result) => {
                  this.setState((prevState) => ({
                    errorsData: addIncrementalPage(prevState.errorsData,
                                                   result.errors_page,
                                                   result.errors_index),
                  }));
                },
                (error) => {
                  console.error("Could not fetch errors page: " + error);
                }
              )
          );
        }

        Promise.all(pagesPending).then(resolveRoundTrip, resolveRoundTrip);
      });
    });
  }

  // Schedules the next poll delay milliseconds from now, replacing any
  // already-scheduled one. Only ever called after the previous round trip
  // (status, plus any finished/errors pages it implied) has fully settled,
  // so at most one round trip is ever outstanding at a time.
  __scheduleNextLoad = (delay) => {
    clearTimeout(this.timeout);
    this.timeout = setTimeout(this.__loadStatus, delay);
  }

  __loadStatus = () => {
    const prevStartTime = this.state.startTime;

    fetchJsonWithTimeout("/MigrateContent?reqfreq=high&output=json&status=status")
      .then(
        (result) => this.updateStateAfterFetch(result, prevStartTime),
        (error) => {
          console.error("Could not fetch status information: " + error);

          this.setState({
            fetchError: true,
            statusList: [ "Waiting for status from LOCKSS 1.0 daemon" ],
            delay: 5000,
          });
        }
      )
      .finally(() => {
        if (this.mounted) {
          this.__scheduleNextLoad(this.state.delay);
        }
      });
  }

  render() {
    return (
        <div>
         {this.StatusList()}
         {this.InstrumentList()}
         {this.ActiveList()}
         <div className="flex-container">
           {this.FinishedList()}
           {this.ErrorList()}
         </div>
      </div>
    );
  }
}

const appContainer = document.getElementById('AuMigrationStatusApp');
ReactDOM.render(React.createElement(AuMigrationStatus), appContainer);
