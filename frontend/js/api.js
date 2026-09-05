/* Thin fetch wrapper for the /api/* JSON REST backend. */
const API = (() => {

  async function request(method, path, body) {
    const opts = {
      method,
      headers: { "Content-Type": "application/json" },
      credentials: "same-origin",
    };
    if (body !== undefined) opts.body = JSON.stringify(body);

    let res;
    try {
      res = await fetch(path, opts);
    } catch (e) {
      throw new ApiError("Could not reach the server. Is it running?", 0);
    }

    let data = null;
    const text = await res.text();
    if (text) {
      try { data = JSON.parse(text); } catch (e) { /* non-json response */ }
    }

    if (!res.ok) {
      const msg = (data && (data.error || data.message)) || `Request failed (${res.status})`;
      throw new ApiError(msg, res.status);
    }
    return data;
  }

  class ApiError extends Error {
    constructor(message, status) {
      super(message);
      this.status = status;
    }
  }

  return {
    get: (path) => request("GET", path),
    post: (path, body) => request("POST", path, body ?? {}),
    put: (path, body) => request("PUT", path, body ?? {}),
    del: (path) => request("DELETE", path),
    ApiError,
  };
})();
