import { useState } from "react";
import { unlockUserByEmail } from "../../api/adminApi.js";

function UnlockUserPage() {
    const [email, setEmail] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (!email.trim()) {
            setError("Email-ul este obligatoriu.");
            return;
        }

        setSubmitting(true);
        setError("");
        setMessage("");
        try {
            await unlockUserByEmail(email.trim());
            setMessage(`Utilizatorul ${email.trim()} a fost deblocat.`);
            setEmail("");
        } catch (err) {
            setError(err.message);
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div>
            <h1>Deblocare utilizator</h1>

            {message && <p style={{ color: "green" }}>{message}</p>}
            {error && <p style={{ color: "red" }}>{error}</p>}

            <form onSubmit={handleSubmit}>
                <div>
                    <label>Email utilizator blocat</label>
                    <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                </div>

                <button type="submit" disabled={submitting}>
                    {submitting ? "Se deblochează..." : "Deblochează utilizator"}
                </button>
            </form>

            <p>
                <a href="/admin/dashboard">Înapoi la dashboard</a>
            </p>
        </div>
    );
}

export default UnlockUserPage;
