function TransactionCard({ transaction }) {
    const formattedDate = new Date(transaction.createdAt).toLocaleString("ro-RO");
    const status = transaction.status?.toLowerCase() || "";
    const statusClass = `status-${status}`;

    const badgeClass = {
        executed: "badge-executed",
        pending_execution: "badge-pending",
        failed: "badge-failed",
        draft: "badge-inactive",
        authorized: "badge-pending",
    }[status] || "badge-inactive";

    return (
        <div className={`transaction-card ${statusClass}`}>
            <div>
                <p><strong>{transaction.transactionType}</strong> — {transaction.description || "fără descriere"}</p>
                <p>Categorie: {transaction.categoryName || "—"} &nbsp;·&nbsp; {formattedDate}</p>
                <p>Din: {transaction.sourceAccountAlias || transaction.sourceAccountIban || "—"} → {transaction.destinationAccountAlias || transaction.destinationAccountIban || transaction.destinationIban || "—"}</p>
            </div>
            <div style={{ textAlign: "right" }}>
                <div className="amount">{transaction.amount?.toLocaleString("ro-RO", { minimumFractionDigits: 2 })} {transaction.currency}</div>
                <span className={`badge ${badgeClass}`}>{transaction.status}</span>
            </div>
        </div>
    );
}

export default TransactionCard;
