import { useState } from "react";
import { createSharedAccount } from "../../api/adminApi.js";
import CreateSharedAccountForm from "../../components/admin/CreateSharedAccountForm.jsx";

function CreateSharedAccountPage() {
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const handleSubmit = async (data) => {
        setSubmitting(true);
        setError("");
        setMessage("");
        try {
            const account = await createSharedAccount(data);
            setMessage(`Contul partajat "${account.alias}" a fost creat.`);
        } catch (err) {
            setError(err.message);
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div>
            <h1>Cont partajat nou</h1>

            {message && <p style={{ color: "green" }}>{message}</p>}
            {error && <p style={{ color: "red" }}>{error}</p>}

            <CreateSharedAccountForm onSubmit={handleSubmit} submitting={submitting} />

            <p>
                <a href="/admin/dashboard">Înapoi la dashboard</a>
            </p>
        </div>
    );
}

export default CreateSharedAccountPage;
