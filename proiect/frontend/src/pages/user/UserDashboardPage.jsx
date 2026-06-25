import { Link, useNavigate, useLocation } from "react-router-dom";
import { useEffect, useState } from "react";
import { getLoggedUser, removeLoggedUser } from "../../utils/authStorage.js";
import { logoutUser } from "../../api/authApi.js";
import { getAccountCurrencySummary, getActiveAccountsPaged, } from "../../api/accountsApi.js";
import { getUserTransactionsPaged, } from "../../api/transactionsApi";
import AccountList from "../../components/accounts/AccountList";
import TransactionList from "../../components/transactions/TransactionList";
import AccountCurrencySummary from "../../components/accounts/AccountCurrencySummary";
import PaginationControls from "../../components/common/PaginationControls";

const EMPTY_ACCOUNTS_PAGE = {
    content: [],
    pageNumber: 0,
    pageSize: 2,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
};

const EMPTY_TRANSACTIONS_PAGE = {
    content: [],
    pageNumber: 0,
    pageSize: 3,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
};

function UserDashboardPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const user = getLoggedUser();

    const [accountsPage, setAccountsPage] = useState(EMPTY_ACCOUNTS_PAGE);
    const [accountSummary, setAccountSummary] = useState([]);
    const [loadingAccounts, setLoadingAccounts] = useState(false);
    const [loadingSummary, setLoadingSummary] = useState(false);
    const [accountsError, setAccountsError] = useState("");
    const [summaryError, setSummaryError] = useState("");
    const [message, setMessage] = useState("");
    const [page, setPage] = useState(0);
    const [pageSize, setPageSize] = useState(2);
    const [sortBy, setSortBy] = useState("alias");
    const [direction, setDirection] = useState("asc");

    const [transactionsPage, setTransactionsPage] = useState(EMPTY_TRANSACTIONS_PAGE);
    const [loadingTransactions, setLoadingTransactions] = useState(false);
    const [transactionsError, setTransactionsError] = useState("");
    const [transactionsPageNumber, setTransactionsPageNumber] = useState(0);
    const [transactionsPageSize, setTransactionsPageSize] = useState(3);
    const [transactionsSortBy, setTransactionsSortBy] = useState("createdAt");
    const [transactionsDirection, setTransactionsDirection] = useState("desc");

    useEffect(() => {
        if (location.state?.message) {
            setMessage(location.state.message);
        }
    }, [location]);

    useEffect(() => {
        if (!user?.userId) {
            return;
        }

        const loadAccountSummary = async () => {
            setLoadingSummary(true);
            setSummaryError("");

            try {
                const response = await getAccountCurrencySummary(user.userId);
                setAccountSummary(response);
            } catch (err) {
                setSummaryError(err.message);
            } finally {
                setLoadingSummary(false);
            }
        };

        loadAccountSummary();
    }, [user?.userId]);

    useEffect(() => {
        if (!user?.userId) {
            return;
        }

        const loadPagedAccounts = async () => {
            setLoadingAccounts(true);
            setAccountsError("");

            try {
                const response = await getActiveAccountsPaged(user.userId, page, pageSize, sortBy, direction);

                setAccountsPage(response);
            } catch (err) {
                setAccountsError(err.message);
            } finally {
                setLoadingAccounts(false);
            }
        };

        loadPagedAccounts();
    }, [user?.userId, page, pageSize, sortBy, direction]);

    useEffect(() => {
        if (!user?.userId) {
            return;
        }

        const loadTransactions = async () => {
            setLoadingTransactions(true);
            setTransactionsError("");

            try {
                const response = await getUserTransactionsPaged(user.userId, transactionsPageNumber, transactionsPageSize, transactionsSortBy, transactionsDirection);

                setTransactionsPage(response);
            } catch (err) {
                setTransactionsError(err.message);
            } finally {
                setLoadingTransactions(false);
            }
        };

        loadTransactions();
    }, [user?.userId, transactionsPageNumber, transactionsPageSize, transactionsSortBy, transactionsDirection]);

    const handleLogout = async () => {
        await logoutUser();
        removeLoggedUser();
        navigate("/login");
    };

    const handleSortByChange = (event) => {
        setSortBy(event.target.value);
        setPage(0);
    };

    const handleDirectionChange = (event) => {
        setDirection(event.target.value);
        setPage(0);
    };

    const handlePageSizeChange = (newPageSize) => {
        setPageSize(newPageSize);
        setPage(0);
    };

    const handleTransactionsSortByChange = (event) => {
        setTransactionsSortBy(event.target.value);
        setTransactionsPageNumber(0);
    };

    const handleTransactionsDirectionChange = (event) => {
        setTransactionsDirection(event.target.value);
        setTransactionsPageNumber(0);
    };

    const handleTransactionsPageSizeChange = (newPageSize) => {
        setTransactionsPageSize(newPageSize);
        setTransactionsPageNumber(0);
    };

    if (!user) {
        return (
            <p>
                User is not logged in.
            </p>
        );
    }

    return (
        <div className="page">
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

            {loadingSummary && <p>Loading account summary...</p>}

            {summaryError && (
                <p style={{ color: "red" }}>
                    {summaryError}
                </p>
            )}

            {!loadingSummary && !summaryError && accountSummary.length > 0 && (
                <AccountCurrencySummary summary={accountSummary} />
            )}

            <div>
                <label>Sort by: </label>

                <select value={sortBy} onChange={handleSortByChange}>
                    <option value="alias">Alias</option>
                    <option value="balance">Balance</option>
                </select>

                <label> Direction: </label>

                <select value={direction} onChange={handleDirectionChange}>
                    <option value="asc">Ascending</option>
                    <option value="desc">Descending</option>
                </select>
            </div>

            {loadingAccounts && <p>Loading accounts...</p>}

            {accountsError && (
                <p style={{ color: "red" }}>
                    {accountsError}
                </p>
            )}

            {!loadingAccounts && !accountsError && (
                <>
                    <AccountList accounts={accountsPage.content} />

                    {accountsPage.totalElements > 0 && (
                        <>
                            <p>
                                Total accounts: {accountsPage.totalElements}
                            </p>

                            <PaginationControls
                                pageNumber={accountsPage.pageNumber}
                                totalPages={accountsPage.totalPages}
                                first={accountsPage.first}
                                last={accountsPage.last}
                                pageSize={pageSize}
                                pageSizeOptions={[2, 4, 6]}
                                onPageChange={setPage}
                                onPageSizeChange={handlePageSizeChange}
                            />
                        </>
                    )}
                </>
            )}

            <hr />

            <h2>Recent Transactions</h2>

            <div>
                <label>Sort by: </label>

                <select value={transactionsSortBy} onChange={handleTransactionsSortByChange}>
                    <option value="createdAt">Date</option>
                    <option value="amount">Amount</option>
                </select>

                <label> Direction: </label>

                <select value={transactionsDirection} onChange={handleTransactionsDirectionChange}>
                    <option value="asc">Ascending</option>
                    <option value="desc">Descending</option>
                </select>
            </div>

            {loadingTransactions && <p>Loading transactions...</p>}

            {transactionsError && (
                <p style={{ color: "red" }}>
                    {transactionsError}
                </p>
            )}

            {!loadingTransactions && !transactionsError && (
                <>
                    <TransactionList transactions={transactionsPage.content} />

                    {transactionsPage.totalElements > 0 && (
                        <PaginationControls
                            pageNumber={transactionsPage.pageNumber}
                            totalPages={transactionsPage.totalPages}
                            first={transactionsPage.first}
                            last={transactionsPage.last}
                            pageSize={transactionsPageSize}
                            pageSizeOptions={[3, 5]}
                            onPageChange={setTransactionsPageNumber}
                            onPageSizeChange={handleTransactionsPageSizeChange}
                        />
                    )}
                </>
            )}

            <hr />

            <h2>Quick Actions</h2>

            <ul className="quick-actions">
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
                    <Link to="/payments/new">
                        Internal / External Payment
                    </Link>
                </li>

                <li>
                    <Link to="/transfer">
                        Transfer Between Own Accounts
                    </Link>
                </li>

                <li>
                    <Link to="/exchange">
                        Currency Exchange
                    </Link>
                </li>

                <li>
                    <Link to="/categories">
                        Categories Management
                    </Link>
                </li>
            </ul>

            <button onClick={handleLogout}>
                Logout
            </button>
        </div>
    );
}

export default UserDashboardPage;