import { useState } from "react";

function SingleAccountCreateForm({ onSubmit }) {
    const [formData, setFormData] = useState({
        alias: "",
        currency: "RON",
        externalIban: "",
        initialAmount: "",
    });

    const handleChange = (event) => {
        const { name, value } = event.target;

        setFormData({
            ...formData,
            [name]: value,
        });
    };

    const handleSubmit = (event) => {
        event.preventDefault();

        onSubmit({
            ...formData,
            initialAmount: Number(formData.initialAmount),
        });
    };

    return (
        <form onSubmit={handleSubmit}>
            <h2>Create Single Account</h2>

            <div>
                <label>Account Alias</label>

                <input
                    type="text"
                    name="alias"
                    value={formData.alias}
                    onChange={handleChange}
                    required
                />
            </div>

            <div>
                <label>Currency</label>

                <select
                    name="currency"
                    value={formData.currency}
                    onChange={handleChange}
                    required
                >
                    <option value="RON">RON</option>
                    <option value="EUR">EUR</option>
                    <option value="USD">USD</option>
                </select>
            </div>

            <div>
                <label>External IBAN</label>

                <input
                    type="text"
                    name="externalIban"
                    value={formData.externalIban}
                    onChange={handleChange}
                    minLength={24}
                    maxLength={24}
                    required
                />
            </div>

            <div>
                <label>Initial Amount</label>

                <input
                    type="number"
                    name="initialAmount"
                    value={formData.initialAmount}
                    onChange={handleChange}
                    min="1"
                    step="0.01"
                    required
                />
            </div>

            <button type="submit">
                Create Account
            </button>
        </form>
    );
}

export default SingleAccountCreateForm;