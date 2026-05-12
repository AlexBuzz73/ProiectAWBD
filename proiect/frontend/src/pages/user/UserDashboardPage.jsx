import { Link, useNavigate, useLocation } from "react-router-dom";
import { useEffect, useState } from "react";
import { getLoggedUser, removeLoggedUser } from "../../utils/authStorage.js";
import { getActiveAccounts } from "../../api/accountsApi.js";
import AccountList from "../../components/accounts/AccountList.jsx";
import AccountCurrencySummary from "../../components/accounts/AccountCurrencySummary.jsx";

function UserDashboardPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const user = getLoggedUser();
    const [accounts, setAccounts] = useState([]);
    const [loadingAccounts, setLoadingAccounts] = useState(false);
    const [accountsError, setAccountsError] = useState("");
    const [message, setMessage] = useState("");
    const [selectedCurrency, setSelectedCurrency] = useState("ALL");
    const filteredAccounts = selectedCurrency === "ALL" ? accounts : accounts.filter((account) => account.currency === selectedCurrency);

    useEffect(() => {
        if (location.state?.message) {
            setMessage(location.state.message);
        }
    }, [location]);

    useEffect(() => {
        loadAccounts();
    }, []);

    const loadAccounts = async () => {
        setLoadingAccounts(true);
        setAccountsError("");

        try {
            const response = await getActiveAccounts(user.userId);

            setAccounts(response);
        } catch (err) {
            setAccountsError(err.message);
        } finally {
            setLoadingAccounts(false);
        }
    };

    const handleLogout = () => {
        removeLoggedUser();
        navigate("/login");
    };

    return (
        <div>
            <h1>User Dashboard</h1>

            <p>
                Welcome, <strong>{user.username}</strong>
            </p>

            <p>Email: {user.email}</p>

            <hr />

            <h2>Accounts</h2>

            {message && (
                <p style={{ color: "green" }}>
                    {message}
                </p>
            )}

            {loadingAccounts && <p>Loading accounts...</p>}

            {accountsError && (
                <p style={{ color: "red" }}>
                    {accountsError}
                </p>
            )}

            {accounts.length > 0 && (
                <>
                    <AccountCurrencySummary accounts={accounts} />

                    <div>
                        <label>Filter by currency</label>

                        <select
                            value={selectedCurrency}
                            onChange={(event) => setSelectedCurrency(event.target.value)}
                        >
                            <option value="ALL">All</option>
                            <option value="RON">RON</option>
                            <option value="EUR">EUR</option>
                            <option value="USD">USD</option>
                        </select>
                    </div>
                </>
            )}

            <AccountList accounts={filteredAccounts} />

            <hr />

            <h2>Recent Transactions</h2>

            <p>Last 5 transactions will be displayed here.</p>

            <hr />

            <h2>Quick Actions</h2>

            <ul>
                <li>
                    <Link to="/accounts/new">
                        Create New Bank Account
                    </Link>
                </li>

                <li>
                    <Link to="/user-limits">
                        Configure User Limits
                    </Link>
                </li>

                <li>
                    Internal / External Payments (Coming soon)
                </li>

                <li>
                    Currency Exchange (Coming soon)
                </li>

                <li>
                    Categories Management (Coming soon)
                </li>
            </ul>

            <button onClick={handleLogout}>
                Logout
            </button>
        </div>
    );
}

export default UserDashboardPage;