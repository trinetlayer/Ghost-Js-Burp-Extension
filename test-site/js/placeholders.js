// GhostJS test bundle — placeholders.js
// These SHOULD be suppressed (false-positive filter). If GhostJS is working,
// none of these should appear as findings.

const examples = {
  aws: "AKIAIOSFODNN7EXAMPLE",                 // AWS doc sample
  awsSecret: "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
  jwt: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
  stripePublishable: "pk_live_51AbCdEfGhIjKlMnOpQrStUv",  // public by design
  yourKey: "your_api_key_here_replace_with_real_value",
  password: "Mot de passe",                    // i18n label, not a secret
  placeholder: "xxxxxxxxxxxxxxxxxxxx"
};

export default examples;
