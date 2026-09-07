import { Link } from "react-router-dom";
import { getLoggedUser } from "../utils/authStorage.js";

function NotFoundPage() {
    const user = getLoggedUser();
    const destination = user ? (user.role === "ADMIN" ? "/admin/dashboard" : "/dashboard") : "/login";
    const label = user ? "Înapoi la Dashboard" : "Mergi la Autentificare";

    return (
        <div style={{ maxWidth: "600px", margin: "4rem auto", padding: "2rem", textAlign: "center" }}>
            <h1>404 - Pagina nu a fost găsită</h1>
            <p style={{ color: "#666", margin: "1rem 0" }}>
                Pagina pe care o căutați nu există sau a fost mutată.
            </p>
            <Link to={destination} style={{ display: "inline-block", marginTop: "1rem" }}>
                {label}
            </Link>
        </div>
    );
}

export default NotFoundPage;
