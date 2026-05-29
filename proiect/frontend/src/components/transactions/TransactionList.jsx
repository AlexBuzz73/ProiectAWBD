import TransactionCard from "./TransactionCard";

function TransactionList({ transactions }) {
    if (transactions.length === 0) {
        return (
            <p>
                No transactions found.
            </p>
        );
    }

    return (
        <div>
            {transactions.map((transaction) => (
                <TransactionCard
                    key={transaction.transactionId}
                    transaction={transaction}
                />
            ))}
        </div>
    );
}

export default TransactionList;