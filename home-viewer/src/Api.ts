import axios from "axios";
import store from "./store";




export function log(message: string) {
  console.log("API => " + message);
}

export function handleError(error: any, errorHandler?: (error: any) => void) {
  if (errorHandler) {
    errorHandler(error);
  }
  console.log("API error => " + error);
}

export function getHome(
  succHandler: (result: any) => void,
  errorHandler?: (error: any) => void
) {
  //log("getting home on " + store.state.serverAddress + "/home");
  return axios
    .get(store.state.serverAddress + "/home")
    .then(response => {
      if (succHandler) {
        succHandler(response.data);
      }
    })
    .catch(error => handleError(error, errorHandler));
}

export function uploadSVG(svg: any, floorName: string, succHandler: (result: any) => void,
                          errorHandler?: (error: any) => void) {
  return axios.post(store.state.serverAddress + "/home/floors/" + floorName + "/actions/svg", svg, {headers: {'Content-Type': 'application/x-www-form-urlencoded'}})
      .then(response => {
        if (succHandler) {
          succHandler(response.data);
        }
      })
      .catch(error => handleError(error, errorHandler));
}

export function getSVG(floorName: string, succHandler: (result: any) => void,
                          errorHandler?: (error: any) => void) {
    return axios.get(store.state.serverAddress + "/home/floors/" + floorName + "/properties/svg/raw")
        .then(response => {
            if (succHandler) {
                succHandler(response.data);
            }
        })
        .catch(error => handleError(error, errorHandler));
}

export function postAction(relativePath: string, payload: string, succHandler: (result: any) => void,
                          errorHandler?: (error: any) => void) {
    return axios.post(store.state.serverAddress + relativePath, payload, {headers: {'Content-Type': 'application/x-www-form-urlencoded'}})
        .then(response => {
            if (succHandler) {
                succHandler(response.data);
            }
        })
        .catch(error => handleError(error, errorHandler));
}
