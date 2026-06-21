import { useEffect, useState } from "react";

function BankLimitsForm({ initialValues, onSubmit, submitting }) {
    const [formData, setFormData] = useState({
        maxAmountPerTransactionRon: "",
        maxDailyAmountRon: "",
        maxDailyTransactionsCount: "",
    });

    const [errors, setErrors] = useState({});

    useEffect(() => {
        setFormData({
            maxAmountPerTransactionRon: initialValues?.maxAmountPerTransactionRon ?? "",
            maxDailyAmountRon: initialValues?.maxDailyAmountRon ?? "",
            maxDailyTransactionsCount: initialValues?.maxDailyTransactionsCount ?? "",
        });
    }, [initialValues]);

    const handleChange = (event) => {
        const { name, value } = event.target;

        setFormData({
            ...formData,
            [name]: value,
        });
    };

    const validateForm = () => {
        const validationErrors = {};

        if (!formData.maxAmountPerTransactionRon) {
            validationErrors.maxAmountPerTransactionRon = "Max amount per transaction is required.";
        } else if (Number(formData.maxAmountPerTransactionRon) <= 0) {
            validationErrors.maxAmountPerTransactionRon = "Max amount per transaction must be greater than zero.";
        }

        if (!formData.maxDailyAmountRon) {
            validationErrors.maxDailyAmountRon = "Max daily amount is required.";
        } else if (Number(formData.maxDailyAmountRon) <= 0) {
            validationErrors.maxDailyAmountRon = "Max daily amount must be greater than zero.";
        }

        if (!formData.maxDailyTransactionsCount) {
            validationErrors.maxDailyTransactionsCount = "Max daily transactions count is required.";
        } else if (Number(formData.maxDailyTransactionsCount) <= 0) {
            validationErrors.maxDailyTransactionsCount = "Max daily transactions count must be greater than zero.";
        } else if (!Number.isInteger(Number(formData.maxDailyTransactionsCount))) {
            validationErrors.maxDailyTransactionsCount = "Max daily transactions count must be an integer.";
        }

        setErrors(validationErrors);

        return Object.keys(validationErrors).length === 0;
    };

    const handleSubmit = (event) => {
        event.preventDefault();

        if (!validateForm()) {
            return;
        }

        onSubmit({
            maxAmountPerTransactionRon: Number(formData.maxAmountPerTransactionRon),
            maxDailyAmountRon: Number(formData.maxDailyAmountRon),
            maxDailyTransactionsCount: Number(formData.maxDailyTransactionsCount),
        });
    };

    return (
        <form onSubmit={handleSubmit}>
            <div>
                <label>Max Amount Per Transaction RON</label>

                <input
                    type="number"
                    name="maxAmountPerTransactionRon"
                    value={formData.maxAmountPerTransactionRon}
                    onChange={handleChange}
                    min="1"
                    step="0.01"
                    required
                />

                {errors.maxAmountPerTransactionRon && (
                    <p style={{ color: "red" }}>{errors.maxAmountPerTransactionRon}</p>
                )}
            </div>

            <div>
                <label>Max Daily Amount RON</label>

                <input
                    type="number"
                    name="maxDailyAmountRon"
                    value={formData.maxDailyAmountRon}
                    onChange={handleChange}
                    min="1"
                    step="0.01"
                    required
                />

                {errors.maxDailyAmountRon && (
                    <p style={{ color: "red" }}>{errors.maxDailyAmountRon}</p>
                )}
            </div>

            <div>
                <label>Max Daily Transactions Count</label>

                <input
                    type="number"
                    name="maxDailyTransactionsCount"
                    value={formData.maxDailyTransactionsCount}
                    onChange={handleChange}
                    min="1"
                    step="1"
                    required
                />

                {errors.maxDailyTransactionsCount && (
                    <p style={{ color: "red" }}>{errors.maxDailyTransactionsCount}</p>
                )}
            </div>

            <button type="submit" disabled={submitting}>
                {submitting ? "Saving limits..." : "Save Bank Limits"}
            </button>
        </form>
    );
}

export default BankLimitsForm;
