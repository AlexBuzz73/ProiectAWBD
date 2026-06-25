import LoginForm from "../components/auth/LoginForm.jsx";
import { loginUser } from "../api/authApi.js";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { getLoggedUser, saveLoggedUser } from "../utils/authStorage.js";

function LoginPage() {
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        if (location.state?.message) {
            setMessage(location.state.message);
        }
    }, [location]);

    useEffect(() => {
        const user = getLoggedUser();

        if (!user) {
            return;
        }

        if (user.role === "ADMIN") {
            navigate("/admin/dashboard");
        } else {
            navigate("/dashboard");
        }
    }, [navigate]);

    const handleLoginSubmit = async (formData) => {
        setLoading(true);
        setMessage("");
        setError("");

        try {
            const user = await loginUser(formData);

            saveLoggedUser(user);

            if(user.role === "ADMIN") {
                navigate("/admin/dashboard")
            } else {
                navigate("/dashboard")
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-card">
                <h1>🏦 Internet Banking</h1>
                <p className="auth-subtitle">Bine ați revenit</p>

                {message && <p style={{ color: "green" }}>{message}</p>}
                {error && <p style={{ color: "red" }}>{error}</p>}
                {loading && <p className="loading">Se procesează...</p>}

                <LoginForm onSubmit={handleLoginSubmit} />

                <p style={{ textAlign: "center", marginTop: "var(--space-md)", fontSize: "0.875rem", color: "var(--text-muted)" }}>
                    Nu ai cont?{" "}
                    <Link to="/register">Înregistrează-te</Link>
                </p>
            </div>
        </div>
    );
}

export default LoginPage;