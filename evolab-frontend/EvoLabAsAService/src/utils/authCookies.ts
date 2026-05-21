export function clearAuthCookies() {
  if (typeof document === "undefined") return;

  const expires = "Thu, 01 Jan 1970 00:00:00 GMT";
  document.cookie = `auth-token=; expires=${expires}; path=/; SameSite=Lax`;
  document.cookie = `XSRF-TOKEN=; expires=${expires}; path=/; SameSite=Lax`;
}
