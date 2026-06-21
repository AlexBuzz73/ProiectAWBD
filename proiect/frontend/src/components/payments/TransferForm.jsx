import { useEffect, useState } from "react";

function TransferForm({ accounts, categories, onSubmit, submitting }) {
    const [formData, setFormData] = useState({
        sourceAccountId: "",
        destinationAccountId: "",
        amount: "",
        categoryId: "",
        description: "",
    });
    const [errors, setErrors] = useState({});
    const [stage, setStage] = useState("details");
    const [password, setPassword] = useState("");

    const eligibleSourceAccounts = accounts.filter((a) => a.accountRole !== "VIEWER");

    useEffect(() => {
        if (eligibleSourceAccounts.length > 0 && !formData.sourceAccountId) {
            setFormData((prev) => ({ ...prev, sourceAccountId: eligibleSourceAccounts[0].accountId }));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [accounts]);

    const sourceAccount = accounts.find((a) => String(a.accountId) === String(formData.sourceAccountId));

    const destinationOptions = accounts.filter(
        (a) =>
            String(a.accountId) !== String(formData.sourceAccountId) &&
            sourceAccount &&
            a.currency === sourceAccount.currency
    );

    const handleChange = (event) => {
        const { name, value } = event.target;

        if (name === "sourceAccountId") {
            setFormData({ ...formData, sourceAccountId: value, destinationAccountId: "" });
            return;
        }

        setFormData({ ...formData, [name]: value });
    };

    const validateDetails = () => {
        const validationErrors = {};

        if (!formData.sourceAccountId) {
            validationErrors.sourceAccountId = "Contul sursă este obligatoriu.";
        }
        if (!formData.destinationAccountId) {
            validationErrors.destinationAccountId = "Contul destinație este obligatoriu.";
        }
        if (!formData.amount || Number(formData.amount) <= 0) {
            validationErrors.amount = "Suma trebuie să fie mai mare decât 0.";
        }
        if (!formData.categoryId) {
            validationErrors.categoryId = "Categoria este obligatorie.";
        }

        setErrors(validationErrors);
        return Object.keys(validationErrors).length === 0;
    };

    const handleContinue = (event) => {
        event.preventDefault();
        if (validateDetails()) {
            setStage("confirm");
        }
    };

    const handleAuthorize = (event) => {
        event.preventDefault();

        if (!password) {
            setErrors({ password: "Parola este obligatorie pentru autorizare." });
            return;
        }

        onSubmit({
            sourceAccountId: Number(formData.sourceAccountId),
            destinationAccountId: Number(formData.destinationAccountId),
            amount: Number(formData.amount),
            categoryId: Number(formData.categoryId),
            description: formData.description,
            password,
        });
    };

    if (eligibleSourceAccounts.length === 0) {
        return <p>Nu ai niciun cont eligibil pentru transfer (rol VIEWER pe toate conturile, sau niciun cont).</p>;
    }

    if (stage === "confirm") {
        const destinationAccount = accounts.find(
            (a) => String(a.accountId) === String(formData.destinationAccountId)
        );

        return (
            <form onSubmit={handleAuthorize}>
                <h2>Confirmă și autorizează transferul</h2>

                <ul>
                    <li>Din: {sourceAccount?.alias} ({sourceAccount?.currency})</li>
                    <li>În: {destinationAccount?.alias} ({destinationAccount?.currency})</li>
                    <li>Sumă: {formData.amount} {sourceAccount?.currency}</li>
                </ul>

                <div>
                    <label>Parolă (pentru autorizare)</label>
                    <input
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                    {errors.password && <p style={{ color: "red" }}>{errors.password}</p>}
                </div>

                <button type="button" onClick={() => setStage("details")} disabled={submitting}>
                    Înapoi
                </button>
                {" "}
                <button type="submit" disabled={submitting}>
                    {submitting ? "Se autorizează..." : "Autorizează transferul"}
                </button>
            </form>
        );
    }

    return (
        <form onSubmit={handleContinue}>
            <h2>Transfer între conturile proprii</h2>

            <div>
                <label>Cont sursă</label>
                <select name="sourceAccountId" value={formData.sourceAccountId} onChange={handleChange} required>
                    {eligibleSourceAccounts.map((account) => (
                        <option key={account.accountId} value={account.accountId}>
                            {account.alias} — {account.currency} — sold {account.balance}
                        </option>
                    ))}
                </select>
                {errors.sourceAccountId && <p style={{ color: "red" }}>{errors.sourceAccountId}</p>}
            </div>

            <div>
                <label>Cont destinație (aceeași valută)</label>
                <select name="destinationAccountId" value={formData.destinationAccountId} onChange={handleChange} required>
                    <option value="">-- selectează --</option>
                    {destinationOptions.map((account) => (
                        <option key={account.accountId} value={account.accountId}>
                            {account.alias} — {account.currency} — sold {account.balance}
                        </option>
                    ))}
                </select>
                {destinationOptions.length === 0 && (
                    <p>Nu ai alt cont activ în aceeași valută ca al contului sursă selectat.</p>
                )}
                {errors.destinationAccountId && <p style={{ color: "red" }}>{errors.destinationAccountId}</p>}
            </div>

            <div>
                <label>Sumă ({sourceAccount?.currency ?? ""})</label>
                <input
                    type="number"
                    name="amount"
                    value={formData.amount}
                    onChange={handleChange}
                    min="0.01"
                    step="0.01"
                    required
                />
                {errors.amount && <p style={{ color: "red" }}>{errors.amount}</p>}
            </div>

            <div>
                <label>Categorie</label>
                <select name="categoryId" value={formData.categoryId} onChange={handleChange} required>
                    <option value="">-- selectează --</option>
                    {categories.map((category) => (
                        <option key={category.categoryId} value={category.categoryId}>
                            {category.name}
                        </option>
                    ))}
                </select>
                {errors.categoryId && <p style={{ color: "red" }}>{errors.categoryId}</p>}
            </div>

            <div>
                <label>Descriere</label>
                <input
                    type="text"
                    name="description"
                    value={formData.description}
                    onChange={handleChange}
                />
            </div>

            <button type="submit">Continuă</button>
        </form>
    );
}

export default TransferForm;
