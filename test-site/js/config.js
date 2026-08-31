// GhostJS test bundle — config.js  (FAKE secrets, not real)

// Google API key (low)
window.MAPS_KEY = "AIzaSyA1234567890abcdefghijklmnopqrstuvw";

// Firebase configuration block (medium)
const firebaseConfig = {
  apiKey: "AIzaSyD9tHk3Lm7Np2Qr5Sv8Wx1Yz4Ab6Cd0Ef",
  authDomain: "demo-app.firebaseapp.com",
  databaseURL: "https://demo-app.firebaseio.com",
  projectId: "demo-app",
  storageBucket: "demo-app.appspot.com",
  messagingSenderId: "1029384756123"
};

// Google OAuth client secret (critical)
const GOOGLE_OAUTH_SECRET = "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";

// Slack bot token (high)
const slack = "xoxb-1234567890-0987654321-Ab1Cd2Ef3Gh4Ij5Kl6Mn7Op8";

// Discovery: internal endpoints + cloud storage
const ENDPOINTS = {
  admin: "/api/internal/admin/config",
  export: "https://demo-assets.s3.amazonaws.com/private/dump.json"
};

export { firebaseConfig, ENDPOINTS };
