// GhostJS test bundle — app.js
// FAKE, format-valid secrets for demoing the scanner. NOT real credentials.
(function () {
  "use strict";

  // Cloud (critical)
  const AWS_ACCESS_KEY = "AKIA1234567890ABCDEF";
  const aws_secret_access_key = "Kx7pQ2mN9vR4tYu8Wz1aB3cD5eF6gH0jL2kM4nPq";

  // Payments (critical)
  const stripeKey = "sk_live_abcdEFGH1234567890ijklMNOP";

  // Source control (critical)
  const githubToken = "ghp_1234567890abcdefghijklmnopqrstuvwxyz12";

  function boot() {
    fetch("/api/v2/users/1024/payment", {
      headers: { Authorization: "Bearer " + githubToken }
    });
    console.log("app booted", AWS_ACCESS_KEY, aws_secret_access_key, stripeKey);
  }

  document.addEventListener("DOMContentLoaded", boot);
})();
