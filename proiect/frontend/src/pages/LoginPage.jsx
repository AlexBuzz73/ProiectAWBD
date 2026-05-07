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
        <div>
            <h1>Internet Banking Login</h1>

            {message && <p style={{ color: "green" }}>{message}</p>}
            {error && <p style={{ color: "red" }}>{error}</p>}
            {loading && <p>Logging in...</p>}

            <LoginForm onSubmit={handleLoginSubmit} />

            <p>
                Don't have an account?{" "}
                <Link to="/register">
                    Register here
                </Link>
            </p>
        </div>
    );
}

export default LoginPage;