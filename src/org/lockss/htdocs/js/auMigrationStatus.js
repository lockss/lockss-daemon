'use strict';

class AuMigrationStatus extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      running: false,
      fetchError: true,
      progress: "0%",
      status: "Loading status",
      delay: 1000,
    };
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
    fetch("/MigrateContent?output=json")
      .then(response => response.json())
      .then(
        (result) => {
          this.setState({
            running: result.running,
            fetchError: false,
            status: result.status,
            progress: result.progress,
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
        <div className="ui-progressbar">
          <div style={{width:this.state.progress}}>{this.state.progress}</div>
        </div>
      </div>
    );
  }
}

const appContainer = document.getElementById('AuMigrationStatusApp');
ReactDOM.render(React.createElement(AuMigrationStatus), appContainer);