'use strict';

class AuMigrationStatus extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      running: false,
      fetchError: true,
      statusList: [ "Loading status" ],
      delay: 1000,
      // finishedPageSize: 1,
      // finishedPageCursor: 0,
      // finishedData: [],
    };
  }

    StatusList() {
        if (this.state.statusList === undefined) {
            return null;
        }
        return (
                <div>Status: {this.state.statusList.map((msg, index) => <div>{msg}<br /></div>)}</div>
        )
    }

    InstrumentList() {
        if (this.state.instrumentList === undefined) {
            return null;
        }
        return (
                <div><font size="2">{this.state.instrumentList.map((msg, index) => <div>{msg}<br /></div>)}</font></div>
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

  // FinishedList() {
  //     const getMoreData = () => {
    //   fetch("/MigrateContent?output=json&status=finished" +
    //                   "&index=" + this.state.finishedPageCursor +
    //                   "&size=" + this.state.finishedPageSize)
    //     .then(response => response.json())
    //     .then(
    //       (result) => {
    //         this.setState({
    //           finishedData: this.state.finishedData.concat(result.finished_page),
    //           finishedPageCursor: this.state.finishedPageCursor + 1,
    //         });
    //       },
    //       (error) => {
    //         console.error("Could not fetch finished AU page: " + error);
    //       }
    //     );
    // }
    //
    // const hasMore = () => {
    //   return this.state.running;
    // }
    //
    //   return (
    //     <InfiniteScroll
    //       dataLength={this.state.finishedData.length}
    //       next={getMoreData}
    //       hasMore={hasMore}
    //       loader={<h4>Loading...</h4>}
    //       height={400}
    //     >
    //       <div>
    //         {this.state.finishedData && this.state.finishedData.map(((msg, index) => (
    //           <div key={index} className="finishedPageRow">
    //             {msg}
    //           </div>
    //         )))
    //         }
    //       </div>
    //     </InfiniteScroll>
    //   );
    // }

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
  }

  componentWillUnmount() {
    clearInterval(this.interval);
  }

  __loadStatus = () => {
    fetch("/MigrateContent?reqfreq=high&output=json&status=status")
      .then(response => response.json())
      .then(
        (result) => {
          this.setState({
            running: result.running,
            fetchError: false,
            statusList: result.status_list,
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
              statusList: [ "Could not fetch status information" ],
              delay: 5000,
            });
        }
      );
  }

  render() {
    return (
      <div>
            {this.StatusList()}
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
