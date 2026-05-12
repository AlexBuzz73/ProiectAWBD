import { Link } from "react-router-dom";

function NotFoundPage() {
    return (
        <div>
            <h1>404 - Page not found</h1>

            <p>The page you are looking for does not exist.</p>

            <Link to="/login">
                Go to login
            </Link>
        </div>
    );
}

export default NotFoundPage;