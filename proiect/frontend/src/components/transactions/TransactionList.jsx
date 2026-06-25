import TransactionCard from "./TransactionCard";

function TransactionList({ transactions }) {
    if (transactions.length === 0) {
        return <p className="empty-state">Nicio tranzacție găsită.</p>;
    }

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-sm)" }}>
            {transactions.map((transaction) => (
                <TransactionCard key={transaction.transactionId} transaction={transaction} />
            ))}
        </div>
    );
}

export default TransactionList;
