import { useEffect, useState } from "react";
import BankLimitsForm from "../components/limits/BankLimitsForm.jsx";
import { getBankLimits, updateBankLimits } from "../api/limitApi.js";

function BankLimitsPage() {
    const [limits, setLimits] = useState(null);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState("");
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    const loadLimits = async () => {
        setLoading(true);
        setLoadError("");
        try {
            const data = await getBankLimits();
            setLimits(data);
        } catch (err) {
            setLoadError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadLimits();
    }, []);

    const handleSubmit = async (formData) => {
        setSubmitting(true);
        setError("");
        setMessage("");
        try {
            const updated = await updateBankLimits(formData);
            setLimits(updated);
            setMessage("Limitele globale au fost actualizate.");
        } catch (err) {
            setError(err.message);
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="page">
            <h1>Configurare limite globale</h1>

            {loading && <p>Se încarcă...</p>}
            {loadError && <p style={{ color: "red" }}>{loadError}</p>}
            {message && <p style={{ color: "green" }}>{message}</p>}
            {error && <p style={{ color: "red" }}>{error}</p>}

            {!loading && !loadError && (
                <BankLimitsForm
                    initialValues={limits}
                    onSubmit={handleSubmit}
                    submitting={submitting}
                />
            )}

            <p>
                <a href="/admin/dashboard">Înapoi la dashboard</a>
            </p>
        </div>
    );
}

export default BankLimitsPage;
