import {useEffect, useState} from "react";
import LimitsForm from "../components/LimitsForm";
import {getBankLimits, updateBankLimits, deleteBankLimits, updateUserLimits, deleteUserLimits} from "../api/limitApi";

function BankLimitsPage() {
    const [limits, setLimits] = useState(null);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    useEffect(() => {
        loadLimits();
    }, []);

    const loadLimits = async () => {
        try {
            const data = await getBankLimits(limits);
            setLimits(data);
            setError("");
        } catch (err) {
            setError(err.message);
        }
    };

    const handleSubmit = async (formatData) => {
        try {
            const updated = await updateBankLimits(limits, formatData);
            setLimits(updated);
            setMessage("Bank Limits updated");
            setError("");
        } catch (err) {
            setError(err.message);
            setMessage("Bank Limits update error");
        }
    }

    const handleDelete = async () => {
        try {
            await deleteBankLimits(limits);
            setMessage("Bank Limits deleted");
            setError("");
            loadLimits();
        } catch (err) {
            setError(err.message);
            setMessage("Bank Limits delete error");
        }
    }

    if (!message) {
        return <p> Loading globl limits...</p>
    }

    return (
        <div>
            <h1>Configure global limits</h1>

            {message && <p style={{ color: "green" }}>{message}</p>}
            {error && <p style={{ color: "red" }}>{error}</p>}

            <LimitsForm
                title="Global bank limits"
                initialValues={limits}
                onSubmit={handleSubmit}
            />

            <button onClick={handleDelete}> Delete Bank Limits</button>
        </div>
    );
}

export default BankLimitsPage;