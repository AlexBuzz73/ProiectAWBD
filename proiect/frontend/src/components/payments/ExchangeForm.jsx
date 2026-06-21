import { useState } from "react";

const SUPPORTED_CURRENCIES = ["RON", "USD", "EUR"];

function isAllowedPair(from, to) {
    if (from === to) return false;
    return from === "RON" || to === "RON";
}

function ExchangeForm({ accounts, categories, onSubmit, submitting }) {
    const [sourceCurrency, setSourceCurrency] = useState("RON");
    const [destinationCurrency, setDestinationCurrency] = useState("USD");
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

    const eligibleAccounts = accounts.filter((a) => a.accountRole !== "VIEWER");
    const sourceOptions = eligibleAccounts.filter((a) => a.currency === sourceCurrency);
    const destinationOptions = accounts.filter(
        (a) => a.currency === destinationCurrency && String(a.accountId) !== String(formData.sourceAccountId)
    );

    const handleCurrencyChange = (field, value) => {
        if (field === "source") {
            setSourceCurrency(value);
        } else {
            setDestinationCurrency(value);
        }
        setFormData({ ...formData, sourceAccountId: "", destinationAccountId: "" });
    };

    const handleChange = (event) => {
        const { name, value } = event.target;
        setFormData({ ...formData, [name]: value });
    };

    const validateDetails = () => {
        const validationErrors = {};

        if (!isAllowedPair(sourceCurrency, destinationCurrency)) {
            validationErrors.pair = "Sistemul permite doar schimburi RON-USD sau RON-EUR.";
        }
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

    if (eligibleAccounts.length === 0) {
        return <p>Nu ai niciun cont eligibil pentru schimb valutar.</p>;
    }

    if (stage === "confirm") {
        const sourceAccount = accounts.find((a) => String(a.accountId) === String(formData.sourceAccountId));
        const destinationAccount = accounts.find((a) => String(a.accountId) === String(formData.destinationAccountId));

        return (
            <form onSubmit={handleAuthorize}>
                <h2>Confirmă și autorizează schimbul valutar</h2>

                <ul>
                    <li>Din: {sourceAccount?.alias} ({sourceAccount?.currency})</li>
                    <li>În: {destinationAccount?.alias} ({destinationAccount?.currency})</li>
                    <li>Sumă: {formData.amount} {sourceAccount?.currency}</li>
                </ul>
                <p>Suma convertită se calculează automat pe baza cursului BNR salvat în sistem.</p>

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
                    {submitting ? "Se autorizează..." : "Autorizează schimbul"}
                </button>
            </form>
        );
    }

    return (
        <form onSubmit={handleContinue}>
            <h2>Schimb valutar</h2>

            <div>
                <label>Valută sursă</label>
                <select value={sourceCurrency} onChange={(e) => handleCurrencyChange("source", e.target.value)}>
                    {SUPPORTED_CURRENCIES.map((c) => (
                        <option key={c} value={c}>{c}</option>
                    ))}
                </select>

                <label> Valută destinație</label>
                <select value={destinationCurrency} onChange={(e) => handleCurrencyChange("destination", e.target.value)}>
                    {SUPPORTED_CURRENCIES.map((c) => (
                        <option key={c} value={c}>{c}</option>
                    ))}
                </select>

                {errors.pair && <p style={{ color: "red" }}>{errors.pair}</p>}
            </div>

            <div>
                <label>Cont sursă ({sourceCurrency})</label>
                <select name="sourceAccountId" value={formData.sourceAccountId} onChange={handleChange} required>
                    <option value="">-- selectează --</option>
                    {sourceOptions.map((account) => (
                        <option key={account.accountId} value={account.accountId}>
                            {account.alias} — sold {account.balance}
                        </option>
                    ))}
                </select>
                {sourceOptions.length === 0 && <p>Nu ai niciun cont activ în {sourceCurrency}.</p>}
                {errors.sourceAccountId && <p style={{ color: "red" }}>{errors.sourceAccountId}</p>}
            </div>

            <div>
                <label>Cont destinație ({destinationCurrency})</label>
                <select name="destinationAccountId" value={formData.destinationAccountId} onChange={handleChange} required>
                    <option value="">-- selectează --</option>
                    {destinationOptions.map((account) => (
                        <option key={account.accountId} value={account.accountId}>
                            {account.alias} — sold {account.balance}
                        </option>
                    ))}
                </select>
                {destinationOptions.length === 0 && <p>Nu ai niciun cont activ în {destinationCurrency}.</p>}
                {errors.destinationAccountId && <p style={{ color: "red" }}>{errors.destinationAccountId}</p>}
            </div>

            <div>
                <label>Sumă ({sourceCurrency})</label>
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

export default ExchangeForm;
