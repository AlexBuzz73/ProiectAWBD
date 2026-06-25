import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { getLoggedUser } from "../../utils/authStorage.js";
import { createSingleAccount } from "../../api/accountsApi.js";
import SingleAccountCreateForm from "../../components/accounts/SingleAccountCreateForm.jsx";

function SingleAccountCreatePage() {
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const user = getLoggedUser();

    const handleCreateAccount = async (formData) => {
        setLoading(true);
        setError("");

        try {
            await createSingleAccount(formData, user.userId);

            navigate("/dashboard", {
                state: {
                    message: "Account created successfully.",
                },
            });
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="page">
            <h1>Create Single Account</h1>

            {error && (
                <p style={{ color: "red" }}>
                    {error}
                </p>
            )}

            {loading && <p>Creating account...</p>}

            <SingleAccountCreateForm onSubmit={handleCreateAccount} />

            <br />

            <Link to="/accounts/new">
                Back to Account Type Selection
            </Link>
        </div>
    );
}

export default SingleAccountCreatePage;