import { useState } from "react";

const ROLES = ["OWNER", "CO_OWNER", "VIEWER"];
const CURRENCIES = ["RON", "USD", "EUR"];

function CreateSharedAccountForm({ onSubmit, submitting }) {
    const [alias, setAlias] = useState("");
    const [currency, setCurrency] = useState("RON");
    const [users, setUsers] = useState([
        { email: "", role: "OWNER" },
        { email: "", role: "CO_OWNER" },
    ]);
    const [errors, setErrors] = useState({});

    const handleUserChange = (index, field, value) => {
        const updated = [...users];
        updated[index] = { ...updated[index], [field]: value };
        setUsers(updated);
    };

    const removeSecondUser = () => {
        setUsers([users[0]]);
    };

    const addSecondUser = () => {
        setUsers([users[0], { email: "", role: "VIEWER" }]);
    };

    const validate = () => {
        const validationErrors = {};

        if (!alias.trim()) {
            validationErrors.alias = "Aliasul contului este obligatoriu.";
        }

        const filledUsers = users.filter((u) => u.email.trim() !== "");

        if (filledUsers.length === 0) {
            validationErrors.users = "Trebuie specificat cel puțin un utilizator.";
        } else if (!filledUsers.some((u) => u.role === "OWNER")) {
            validationErrors.users = "Contul trebuie să aibă cel puțin un utilizator cu rolul OWNER.";
        }

        setErrors(validationErrors);
        return Object.keys(validationErrors).length === 0;
    };

    const handleSubmit = (event) => {
        event.preventDefault();
        if (!validate()) return;

        onSubmit({
            alias: alias.trim(),
            currency,
            users: users
                .filter((u) => u.email.trim() !== "")
                .map((u) => ({ email: u.email.trim(), role: u.role })),
        });
    };

    return (
        <form onSubmit={handleSubmit}>
            <div>
                <label>Alias cont</label>
                <input type="text" value={alias} onChange={(e) => setAlias(e.target.value)} required />
                {errors.alias && <p style={{ color: "red" }}>{errors.alias}</p>}
            </div>

            <div>
                <label>Valută</label>
                <select value={currency} onChange={(e) => setCurrency(e.target.value)}>
                    {CURRENCIES.map((c) => (
                        <option key={c} value={c}>{c}</option>
                    ))}
                </select>
            </div>

            <h3>Utilizator 1</h3>
            <div>
                <label>Email</label>
                <input
                    type="email"
                    value={users[0].email}
                    onChange={(e) => handleUserChange(0, "email", e.target.value)}
                    required
                />

                <label> Rol</label>
                <select value={users[0].role} onChange={(e) => handleUserChange(0, "role", e.target.value)}>
                    {ROLES.map((r) => (
                        <option key={r} value={r}>{r}</option>
                    ))}
                </select>
            </div>

            {users.length === 2 ? (
                <>
                    <h3>Utilizator 2</h3>
                    <div>
                        <label>Email</label>
                        <input
                            type="email"
                            value={users[1].email}
                            onChange={(e) => handleUserChange(1, "email", e.target.value)}
                        />

                        <label> Rol</label>
                        <select value={users[1].role} onChange={(e) => handleUserChange(1, "role", e.target.value)}>
                            {ROLES.map((r) => (
                                <option key={r} value={r}>{r}</option>
                            ))}
                        </select>
                    </div>
                    <button type="button" onClick={removeSecondUser}>Elimină al doilea utilizator</button>
                </>
            ) : (
                <button type="button" onClick={addSecondUser}>Adaugă al doilea utilizator</button>
            )}

            {errors.users && <p style={{ color: "red" }}>{errors.users}</p>}

            <div>
                <button type="submit" disabled={submitting}>
                    {submitting ? "Se creează..." : "Creează cont partajat"}
                </button>
            </div>
        </form>
    );
}

export default CreateSharedAccountForm;
