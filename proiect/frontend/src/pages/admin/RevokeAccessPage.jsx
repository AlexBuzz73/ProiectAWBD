import { useState } from "react";
import { revokeAccountAccess } from "../../api/adminApi.js";

function RevokeAccessPage() {
    const [accountId, setAccountId] = useState("");
    const [email, setEmail] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (!accountId || !email.trim()) {
            setError("ID-ul contului și email-ul sunt obligatorii.");
            return;
        }

        setSubmitting(true);
        setError("");
        setMessage("");
        try {
            await revokeAccountAccess(Number(accountId), email.trim());
            setMessage(`Accesul lui ${email.trim()} la contul ${accountId} a fost revocat.`);
            setEmail("");
        } catch (err) {
            setError(err.message);
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="page">
            <h1>Revocare acces cont partajat</h1>

            <p>
                Notă: introdu ID-ul contului (vizibil în baza de date / în detaliile contului) și email-ul
                utilizatorului căruia vrei să-i revoci accesul. O listă ghidată de conturi partajate poate fi
                adăugată ulterior printr-un endpoint dedicat de listare.
            </p>

            {message && <p style={{ color: "green" }}>{message}</p>}
            {error && <p style={{ color: "red" }}>{error}</p>}

            <form onSubmit={handleSubmit}>
                <div>
                    <label>ID cont</label>
                    <input
                        type="number"
                        value={accountId}
                        onChange={(e) => setAccountId(e.target.value)}
                        required
                    />
                </div>

                <div>
                    <label>Email utilizator</label>
                    <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                </div>

                <button type="submit" disabled={submitting}>
                    {submitting ? "Se revocă..." : "Revocă accesul"}
                </button>
            </form>

            <p>
                <a href="/admin/dashboard">Înapoi la dashboard</a>
            </p>
        </div>
    );
}

export default RevokeAccessPage;
