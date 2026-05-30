import { Link, useParams, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { getLoggedUser } from "../../utils/authStorage.js";
import { closeAccount, getAccountDetails } from "../../api/accountsApi.js";
import PaginationControls from "../../components/common/PaginationControls";
import TransactionList from "../../components/transactions/TransactionList";
import { getAccountTransactionsPaged } from "../../api/transactionsApi";
import AccountCardSection from "../../components/cards/AccountCardSection.jsx";

const EMPTY_TRANSACTIONS_PAGE = {
    content: [],
    pageNumber: 0,
    pageSize: 3,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
};

function AccountDetailsPage() {
    const { accountId } = useParams();
    const navigate = useNavigate();
    const user = getLoggedUser();
    const [account, setAccount] = useState(null);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [closing, setClosing] = useState(false);

    const [transactionsPage, setTransactionsPage] = useState(EMPTY_TRANSACTIONS_PAGE);
    const [loadingTransactions, setLoadingTransactions] = useState(false);
    const [transactionsError, setTransactionsError] = useState("");
    const [transactionsPageNumber, setTransactionsPageNumber] = useState(0);
    const [transactionsPageSize, setTransactionsPageSize] = useState(3);
    const [transactionsSortBy, setTransactionsSortBy] = useState("createdAt");
    const [transactionsDirection, setTransactionsDirection] = useState("desc");

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

    useEffect(() => {
        if (!user?.userId) {
            return;
        }

        const loadTransactions = async () => {
            setLoadingTransactions(true);
            setTransactionsError("");

            try {
                const response = await getAccountTransactionsPaged(accountId, user.userId, transactionsPageNumber, transactionsPageSize, transactionsSortBy, transactionsDirection);

                setTransactionsPage(response);
            } catch (err) {
                setTransactionsError(err.message);
            } finally {
                setLoadingTransactions(false);
            }
        };

        loadTransactions();
    }, [accountId, user?.userId, transactionsPageNumber, transactionsPageSize, transactionsSortBy, transactionsDirection,]);

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

                    <AccountCardSection
                        userId={user.userId}
                        account={account}
                    />

                    <hr />

                    <h2>Transactions</h2>

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