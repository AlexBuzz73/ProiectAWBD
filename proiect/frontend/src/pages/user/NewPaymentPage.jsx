import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getLoggedUser } from "../../utils/authStorage.js";
import { getActiveAccounts } from "../../api/accountsApi.js";
import { getCategories } from "../../api/categoriesApi.js";
import { initiatePayment } from "../../api/paymentsApi.js";
import PaymentForm from "../../components/payments/PaymentForm.jsx";

function NewPaymentPage() {
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

    const handleSubmit = async (paymentData) => {
        setSubmitting(true);
        setSubmitError("");
        try {
            await initiatePayment(user.userId, paymentData);
            navigate("/dashboard", { state: { message: "Plata a fost inițiată cu succes." } });
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
        <div className="page">
            <h1>Plată nouă</h1>

            {loading && <p>Se încarcă...</p>}
            {loadError && <p style={{ color: "red" }}>{loadError}</p>}
            {submitError && <p style={{ color: "red" }}>{submitError}</p>}

            {!loading && !loadError && (
                <PaymentForm
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

export default NewPaymentPage;
