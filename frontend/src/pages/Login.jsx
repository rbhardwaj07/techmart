import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      await login(form.email, form.password);
      navigate(location.state?.from || "/", { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <h1>Log in</h1>
      {error && <p className="message error">{error}</p>}
      <label>
        Email
        <input type="email" required value={form.email}
               onChange={(e) => setForm({ ...form, email: e.target.value })} />
      </label>
      <label>
        Password
        <input type="password" required value={form.password}
               onChange={(e) => setForm({ ...form, password: e.target.value })} />
      </label>
      <button type="submit" className="btn" disabled={busy}>{busy ? "Logging in..." : "Log in"}</button>
      <p className="auth-alt">New here? <Link to="/register">Create an account</Link></p>
    </form>
  );
}
