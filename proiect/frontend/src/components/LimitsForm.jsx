import {useEffect, useState} from "react";

function LimitsForm({ title, initialValues, onSubmit}) {
    const [formData, setFormData] = useState({
        maxAmountPerTransactionRon: "",
        maxDailyAmountRon: "",
        maxDailyTransactionsCount: ""
    });

    useEffect(() => {
        if (initialValues) {
            setFormData({
                maxAmountPerTransactionRon: initialValues.maxAmountPerTransactionRon || "",
                maxDailyAmountRon: initialValues.maxDailyAmountRon || "",
                maxDailyTransactionsCount: initialValues.maxDailyTransactionsCount || "",
            });
        }
    }, [initialValues]);

    const handleChange = (e) => {
        const {name, value} = e.target;

        setFormData({
            ...formData,
            [name]: value,
        });
    };

    const handleSubmit = (e) => {
        e.preventDefault();

        onSubmit({
            maxAmountPerTransactionRon: Number(formData.maxAmountPerTransactionRon),
            maxDailyAmountRon: Number(formData.maxDailyAmountRon),
            maxDailyTransactionsCount: Number(formData.maxDailyTransactionsCount),
        });
    };

    return (
        <form onSubmit={handleSubmit}>
            <h2>{title}</h2>

            <div>
                <label>Maximum limit per transaction RON</label>
                <input
                    type="number"
                    name="maxAmountPerTransactionRon"
                    value={formData.maxAmountPerTransactionRon}
                    onChange={handleChange}
                    required
                    />
            </div>

            <div>
                <label>Maximum daily limit RON</label>
                <input
                    type="number"
                    name="maxDailyAmountRon"
                    value={formData.maxDailyAmountRon}
                    onChange={handleChange}
                    required
                />
            </div>

            <div>
                <label>Maximum daily transactions Count</label>
                <input
                    type="number"
                    name="maxDailyTransactionsCount"
                    value={formData.maxDailyTransactionsCount}
                    onChange={handleChange}
                    required
                />
            </div>
            <button type="submit">Submit</button>
        </form>
    );
}

export default LimitsForm;