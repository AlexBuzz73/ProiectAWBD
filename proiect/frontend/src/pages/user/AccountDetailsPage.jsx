import { Link, useParams, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { getLoggedUser } from "../../utils/authStorage.js";
import { closeAccount, getAccountDetails } from "../../api/accountsApi.js";

function AccountDetailsPage() {
    const { accountId } = useParams();
    const navigate = useNavigate();
    const user = getLoggedUser();
    const [account, setAccount] = useState(null);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [closing, setClosing] = useState(false);

    useEffect(() => {
        loadAccountDetails();
    }, []);

    const loadAccountDetails = async () => {
        setLoading(true);
        setError("");

        try {
            const response = await getAccountDetails(accountId, user.userId);

            setAccount(response);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleCloseAccount = async () => {
        const confirmed = window.confirm("Are you sure you want to close this account?");

        if (!confirmed) {
            return;
        }

        setClosing(true);
        setError("");

        try {
            await closeAccount(accountId, user.userId);

            navigate("/dashboard", {
                state: {
                    message: "Account closed successfully.",
                },
            });
        } catch (err) {
            setError(err.message);
        } finally {
            setClosing(false);
        }
    };

    return (
        <div>
            <h1>Account Details</h1>

            {error && (
                <p style={{ color: "red" }}>
                    {error}
                </p>
            )}

            {loading && <p>Loading account details...</p>}

            {account && (
                <div>
                    <p>
                        <strong>Alias:</strong> {account.alias}
                    </p>

                    <p>
                        <strong>IBAN:</strong> {account.iban}
                    </p>

                    <p>
                        <strong>Currency:</strong> {account.currency}
                    </p>

                    <p>
                        <strong>Balance:</strong> {account.balance}
                    </p>

                    <p>
                        <strong>Your role:</strong> {account.accountRole}
                    </p>

                    <hr />

                    <h2>Transactions</h2>

                    <p>
                        Transaction history will be displayed here.
                    </p>

                    <hr />

                    {account.accountRole === "OWNER" && (
                        <button
                            type="button"
                            onClick={handleCloseAccount}
                            disabled={closing}
                        >
                            {closing ? "Closing account..." : "Close Account"}
                        </button>
                    )}
                </div>
            )}

            <br />

            <Link to="/dashboard">
                Back to Dashboard
            </Link>
        </div>
    );
}

export default AccountDetailsPage;