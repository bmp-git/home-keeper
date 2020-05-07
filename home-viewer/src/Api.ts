import axios from "axios";

export const server = "http://127.0.0.1:8090/api";

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
  log("getting home on " + server + "/home");
  return axios
    .get(server + "/home")
    .then(response => {
      if (succHandler) {
        succHandler(response.data);
      }
    })
    .catch(error => handleError(error, errorHandler));
}

export function uploadSVG(svg: any, floorName: string, succHandler: (result: any) => void,
                          errorHandler?: (error: any) => void) {
  return axios.post(server + "/home/floors/" + floorName + "/actions/svg", svg, {headers: {'Content-Type': 'application/x-www-form-urlencoded'}})
      .then(response => {
        if (succHandler) {
          succHandler(response.data);
        }
      })
      .catch(error => handleError(error, errorHandler));
}

export function getSVG(floorName: string, succHandler: (result: any) => void,
                          errorHandler?: (error: any) => void) {
    return axios.get(server + "/home/floors/" + floorName + "/properties/svg")
        .then(response => {
            if (succHandler) {
                succHandler(response.data);
            }
        })
        .catch(error => handleError(error, errorHandler));
}
