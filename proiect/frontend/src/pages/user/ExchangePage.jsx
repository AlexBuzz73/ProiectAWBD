import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getLoggedUser } from "../../utils/authStorage.js";
import { getActiveAccounts } from "../../api/accountsApi.js";
import { getCategories } from "../../api/categoriesApi.js";
import { exchangeCurrency } from "../../api/paymentsApi.js";
import ExchangeForm from "../../components/payments/ExchangeForm.jsx";

function ExchangePage() {
    const navigate = useNavigate();
    const user = getLoggedUser();

    const [accounts, setAccounts] = useState([]);
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState("");

    useEffect(() => {
        if (!user?.userId) return;

        const loadData = async () => {
            setLoading(true);
            setLoadError("");
            try {
                const [accountsData, categoriesData] = await Promise.all([
                    getActiveAccounts(user.userId),
                    getCategories(user.userId),
                ]);
                setAccounts(accountsData);
                setCategories(categoriesData);
            } catch (err) {
                setLoadError(err.message);
            } finally {
                setLoading(false);
            }
        };

        loadData();
    }, [user?.userId]);

    const handleSubmit = async (exchangeData) => {
        setSubmitting(true);
        setSubmitError("");
        try {
            await exchangeCurrency(user.userId, exchangeData);
            navigate("/dashboard", { state: { message: "Schimbul valutar a fost efectuat cu succes." } });
        } catch (err) {
            setSubmitError(err.message);
        } finally {
            setSubmitting(false);
        }
    };

    if (!user) {
        return <p>User is not logged in.</p>;
    }

    return (
        <div>
            <h1>Schimb valutar</h1>

            {loading && <p>Se încarcă...</p>}
            {loadError && <p style={{ color: "red" }}>{loadError}</p>}
            {submitError && <p style={{ color: "red" }}>{submitError}</p>}

            {!loading && !loadError && (
                <ExchangeForm
                    accounts={accounts}
                    categories={categories}
                    onSubmit={handleSubmit}
                    submitting={submitting}
                />
            )}

            <p>
                <a href="/dashboard">Înapoi la dashboard</a>
            </p>
        </div>
    );
}

export default ExchangePage;
