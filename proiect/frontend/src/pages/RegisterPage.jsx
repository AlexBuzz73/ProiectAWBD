import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getLoggedUser } from "../utils/authStorage.js";
import IndividualRegistrationForm from "../components/auth/IndividualRegistrationForm.jsx";
import UserRegistrationForm from "../components/auth/UserRegistrationForm.jsx";
import { registerUser, validateIndividual } from "../api/authApi.js";

function RegisterPage() {
    const [step, setStep] = useState(1);
    const [individualData, setIndividualData] = useState(null);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

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

    const handleIndividualSubmit = async (formData) => {
        setLoading(true);
        setMessage("");
        setError("");

        try {
            await validateIndividual(formData);

            setIndividualData(formData);
            setStep(2);
            setMessage("Individual data validated successfully. Please set your credentials.");
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleUserSubmit = async (userData) => {
        setLoading(true);
        setMessage("");
        setError("");

        try {
            await registerUser({
                individual: individualData,
                user: userData,
            });

            navigate("/login", {
                state: {
                    message: "Account created successfully. You can now login.",
                },
            });
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleBack = () => {
        setStep(1);
        setMessage("");
        setError("");
    };

    return (
        <div className="auth-page">
            <div className="auth-card">
                <h1>🏦 Internet Banking</h1>
                <p className="auth-subtitle">
                    {step === 1 ? "Pasul 1 din 2 — Date personale" : "Pasul 2 din 2 — Date cont"}
                </p>

                {message && <p style={{ color: "green" }}>{message}</p>}
                {error && <p style={{ color: "red" }}>{error}</p>}
                {loading && <p className="loading">Se procesează...</p>}

                {step === 1 && (
                    <IndividualRegistrationForm onSubmit={handleIndividualSubmit} />
                )}

                {step === 2 && (
                    <UserRegistrationForm onSubmit={handleUserSubmit} onBack={handleBack} />
                )}

                <p style={{ textAlign: "center", marginTop: "var(--space-md)", fontSize: "0.875rem", color: "var(--text-muted)" }}>
                    Ai deja cont?{" "}
                    <Link to="/login">Autentifică-te</Link>
                </p>
            </div>
        </div>
    );
}

export default RegisterPage;