import { useEffect, useState } from "react";

function tomorrowDateString() {
    const date = new Date();
    date.setDate(date.getDate() + 1);
    return date.toISOString().split("T")[0];
}

function PaymentForm({ accounts, categories, onSubmit, submitting }) {
    const [formData, setFormData] = useState({
        sourceAccountId: "",
        destinationIban: "",
        amount: "",
        categoryId: "",
        processingType: "STANDARD",
        scheduledDate: "",
        description: "",
        tagIds: [],
    });
    const [errors, setErrors] = useState({});
    const [stage, setStage] = useState("details");
    const [password, setPassword] = useState("");

    const eligibleAccounts = accounts.filter((a) => a.accountRole !== "VIEWER");
    const selectedAccount = eligibleAccounts.find(
        (a) => String(a.accountId) === String(formData.sourceAccountId)
    );

    useEffect(() => {
        if (eligibleAccounts.length > 0 && !formData.sourceAccountId) {
            setFormData((prev) => ({ ...prev, sourceAccountId: eligibleAccounts[0].accountId }));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [accounts]);

    const handleChange = (event) => {
        const { name, value } = event.target;
        setFormData({ ...formData, [name]: value });
    };

    const validateDetails = () => {
        const validationErrors = {};

        if (!formData.sourceAccountId) {
            validationErrors.sourceAccountId = "Contul sursă este obligatoriu.";
        }
        if (!formData.destinationIban || formData.destinationIban.trim() === "") {
            validationErrors.destinationIban = "IBAN-ul destinație este obligatoriu.";
        }
        if (!formData.amount || Number(formData.amount) <= 0) {
            validationErrors.amount = "Suma trebuie să fie mai mare decât 0.";
        }
        if (!formData.categoryId) {
            validationErrors.categoryId = "Categoria este obligatorie.";
        }
        if (formData.processingType === "PROGRAMAT") {
            if (!formData.scheduledDate) {
                validationErrors.scheduledDate = "Data execuției este obligatorie pentru plățile programate.";
            } else if (formData.scheduledDate <= new Date().toISOString().split("T")[0]) {
                validationErrors.scheduledDate = "Data trebuie să fie în viitor.";
            }
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

    const handleBack = () => {
        setStage("details");
    };

    const handleAuthorize = (event) => {
        event.preventDefault();

        if (!password) {
            setErrors({ password: "Parola este obligatorie pentru autorizare." });
            return;
        }

        onSubmit({
            sourceAccountId: Number(formData.sourceAccountId),
            destinationIban: formData.destinationIban.trim(),
            amount: Number(formData.amount),
            currency: selectedAccount?.currency ?? "",
            categoryId: Number(formData.categoryId),
            processingType: formData.processingType,
            scheduledDate: formData.processingType === "PROGRAMAT" ? formData.scheduledDate : null,
            description: formData.description,
            tagIds: [],
            password,
        });
    };

    if (eligibleAccounts.length === 0) {
        return <p>Nu ai niciun cont eligibil pentru inițierea unei plăți (rol VIEWER pe toate conturile, sau niciun cont).</p>;
    }

    if (stage === "confirm") {
        return (
            <form onSubmit={handleAuthorize}>
                <h2>Confirmă și autorizează plata</h2>

                <ul>
                    <li>Cont sursă: {selectedAccount?.alias} ({selectedAccount?.currency})</li>
                    <li>IBAN destinație: {formData.destinationIban}</li>
                    <li>Sumă: {formData.amount} {selectedAccount?.currency}</li>
                    <li>Tip procesare: {formData.processingType}</li>
                    {formData.processingType === "PROGRAMAT" && (
                        <li>Data execuției: {formData.scheduledDate}</li>
                    )}
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

                <button type="button" onClick={handleBack} disabled={submitting}>
                    Înapoi
                </button>
                {" "}
                <button type="submit" disabled={submitting}>
                    {submitting ? "Se autorizează..." : "Autorizează plata"}
                </button>
            </form>
        );
    }

    return (
        <form onSubmit={handleContinue}>
            <h2>Plată nouă</h2>

            <div>
                <label>Cont sursă</label>
                <select name="sourceAccountId" value={formData.sourceAccountId} onChange={handleChange} required>
                    {eligibleAccounts.map((account) => (
                        <option key={account.accountId} value={account.accountId}>
                            {account.alias} — {account.currency} — sold {account.balance}
                        </option>
                    ))}
                </select>
                {errors.sourceAccountId && <p style={{ color: "red" }}>{errors.sourceAccountId}</p>}
            </div>

            <div>
                <label>IBAN destinație</label>
                <input
                    type="text"
                    name="destinationIban"
                    value={formData.destinationIban}
                    onChange={handleChange}
                    required
                />
                {errors.destinationIban && <p style={{ color: "red" }}>{errors.destinationIban}</p>}
            </div>

            <div>
                <label>Sumă ({selectedAccount?.currency ?? ""})</label>
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
                <label>Tip procesare</label>
                <select name="processingType" value={formData.processingType} onChange={handleChange}>
                    <option value="STANDARD">Standard</option>
                    <option value="URGENT">Urgent</option>
                    <option value="PROGRAMAT">Programat</option>
                </select>
            </div>

            {formData.processingType === "PROGRAMAT" && (
                <div>
                    <label>Data execuției</label>
                    <input
                        type="date"
                        name="scheduledDate"
                        value={formData.scheduledDate}
                        onChange={handleChange}
                        min={tomorrowDateString()}
                        required
                    />
                    {errors.scheduledDate && <p style={{ color: "red" }}>{errors.scheduledDate}</p>}
                </div>
            )}

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

export default PaymentForm;
