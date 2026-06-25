import AccountCard from "./AccountCard.jsx";

function AccountList({ accounts }) {
    if (accounts.length === 0) {
        return <p className="empty-state">Nu ai niciun cont activ.</p>;
    }

    return (
        <div className="grid-cards">
            {accounts.map((account) => (
                <AccountCard key={account.accountId} account={account} />
            ))}
        </div>
    );
}

export default AccountList;
