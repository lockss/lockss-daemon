'use strict';

class AuMigrationStatus extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      running: false,
      fetchError: true,
      status: "Loading status",
      delay: 1000,
    };
  }

    InstrumentList() {
        if (this.state.instrumentList === undefined) {
            return null;
        }
        return (
                <div><font size="2">{this.state.instrumentList.map((msg, index) =>  <div>{msg}<br /></div>)}</font></div>
        )
    }

    ActiveList() {
        if (this.state.activeList === undefined) {
            return null;
        }
        return (
                <div>Active: <ul>{this.state.activeList.map((msg, index) =>  <li key={index}>{msg}</li>)}</ul></div>
        )
    }

    FinishedList() {
        if (this.state.finishedList === undefined) {
            return null;
        }
        return (
                <div>Finished: <ul>{this.state.finishedList.map((msg, index) =>  <li key={index}>{msg}</li>)}</ul></div>
        )
    }

    ErrorList() {
        if (this.state.errors === undefined) {
            return null;
        }
        return (
                <div>Errors: <ul>{this.state.errors.map((msg, index) =>  <li key={index}>{msg}</li>)}</ul></div>
        )
    }

  componentDidMount() {
    this.__loadStatus();
    this.interval = setInterval(this.__loadStatus, this.state.delay);
  }

  componentDidUpdate(prevProps, prevState) {
    if (prevState.delay != this.state.delay) {
      clearInterval(this.interval);
      this.interval = setInterval(this.__loadStatus, this.state.delay);
    }

    // FIXME: Replace with a jQuery solution?
    if (prevState.running != this.state.running) {
      for (const e of document.querySelectorAll("input[type='submit']")) {
        if (this.state.running) {
          e.setAttribute("disabled", "disabled");
        } else {
          e.removeAttribute("disabled");
        }
      }
    }
  }

  componentWillUnmount() {
    clearInterval(this.interval);
  }

  __loadStatus = () => {
    fetch("/MigrateContent?reqfreq=high&output=json")
      .then(response => response.json())
      .then(
        (result) => {
          this.setState({
            running: result.running,
            fetchError: false,
            status: result.status,
            instrumentList: result.instrument_list,
            activeList: result.active_list,
            finishedList: result.finished_list,
            errors: result.errors,
            delay: result.running ? 1000 : 5000,
          });
        },
        (error) => {
            console.error("Could not fetch status information: " + error);

            this.setState({
              fetchError: true,
              status: "Could not fetch status information",
              delay: 5000,
            });
        }
      );
  }

  render() {
    return (
      <div>
        <div>Running: {this.state.fetchError ? "Unknown" : this.state.running ? "Yes" : "No"}</div>
        <div>Status: {this.state.status}</div>
            {this.InstrumentList()}
            {this.ActiveList()}
            {this.FinishedList()}
            {this.ErrorList()}
      </div>
    );
  }
}

const appContainer = document.getElementById('AuMigrationStatusApp');
ReactDOM.render(React.createElement(AuMigrationStatus), appContainer);
