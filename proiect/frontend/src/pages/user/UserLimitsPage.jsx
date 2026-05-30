import { Link } from "react-router-dom";
import { useEffect, useState } from "react";
import { getLoggedUser } from "../../utils/authStorage.js";
import { getUserLimits, updateUserLimits } from "../../api/limitsApi.js";
import UserLimitsForm from "../../components/limits/UserLimitsForm.jsx";

const EMPTY_LIMITS = {
    maxAmountPerTransactionRon: "",
    maxDailyAmountRon: "",
    maxDailyTransactionsCount: "",
};

function UserLimitsPage() {
    const user = getLoggedUser();
    const [limits, setLimits] = useState(EMPTY_LIMITS);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    useEffect(() => {
        loadUserLimits();
    }, []);

    const normalizeLimits = (response) => {
        return {
            maxAmountPerTransactionRon: response?.maxAmountPerTransactionRon ?? "",
            maxDailyAmountRon: response?.maxDailyAmountRon ?? "",
            maxDailyTransactionsCount: response?.maxDailyTransactionsCount ?? "",
        };
    };

    const loadUserLimits = async () => {
        setLoading(true);
        setError("");
        setSuccessMessage("");

        try {
            const response = await getUserLimits(user.userId);

            setLimits(normalizeLimits(response));
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (limitsData) => {
        setSaving(true);
        setError("");
        setSuccessMessage("");

        try {
            const response = await updateUserLimits(user.userId, limitsData);

            setLimits(normalizeLimits(response));
            setSuccessMessage("Transaction limits saved successfully.");
        } catch (err) {
            setError(err.message);
        } finally {
            setSaving(false);
        }
    };

    return (
        <div>
            <h1>Manage Transaction Limits</h1>

            <p>
                Configure your personal transaction limits. All values are expressed in RON.
            </p>

            {error && (
                <p style={{ color: "red" }}>
                    {error}
                </p>
            )}

            {successMessage && (
                <p style={{ color: "green" }}>
                    {successMessage}
                </p>
            )}

            {loading && <p>Loading limits...</p>}

            {!loading && (
                <UserLimitsForm
                    initialValues={limits}
                    onSubmit={handleSubmit}
                    submitting={saving}
                />
            )}

            <br />

            <Link to="/dashboard">
                Back to Dashboard
            </Link>
        </div>
    );
}

export default UserLimitsPage;