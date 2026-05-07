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
        <div>
            <h1>Register</h1>

            {message && <p style={{ color: "green" }}>{message}</p>}
            {error && <p style={{ color: "red" }}>{error}</p>}
            {loading && <p>Processing request...</p>}

            {step === 1 && (
                <IndividualRegistrationForm
                    onSubmit={handleIndividualSubmit}
                />
            )}

            {step === 2 && (
                <UserRegistrationForm
                    onSubmit={handleUserSubmit}
                    onBack={handleBack}
                />
            )}

            <p>
                Already have an account?{" "}
                <Link to="/login">
                    Login here
                </Link>
            </p>
        </div>
    );
}

export default RegisterPage;