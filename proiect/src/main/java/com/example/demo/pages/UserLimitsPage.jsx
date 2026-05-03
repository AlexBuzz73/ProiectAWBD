import {useEffect, useState} from "react";
import LimitsForm from "../components/LimitsForm";
import {getUserLimits, updateUserLimits, deleteUserLimits} from "../api/limitApi";

function UserLimitsPage() {
    const userId = 1; //testare
    const [limits, setLimits] = useState(null);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    useEffect(() => {
        loadLimits();
    }, []);

    const loadLimits = async () => {
        try {
            const data = await getUserLimits(limits);
            setLimits(data);
            setError("");
        } catch (err) {
            setError(err.message);
        }
    };

    const handleSubmit = async (formatData) => {
        try {
            const updated = await updateUserLimits(limits, formatData);
            setLimits(updated);
            setMessage("User Limits updated");
            setError("");
        } catch (err) {
            setError(err.message);
            setMessage("User Limits update error");
        }
    }

    const handleDelete = async () => {
        try {
            await deleteUserLimits(limits);
            setMessage("User Limits deleted");
            setError("");
            loadLimits();
        } catch (err) {
            setError(err.message);
            setMessage("User Limits delete error");
        }
    }

    if (!message) {
        return <p> Loading global limits...</p>
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

            <button onClick={handleDelete}> Delete User Limits</button>
        </div>
    );
}

export default UserLimitsPage;