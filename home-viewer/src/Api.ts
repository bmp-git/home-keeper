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
