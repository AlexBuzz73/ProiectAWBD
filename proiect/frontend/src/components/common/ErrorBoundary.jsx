import React from "react";

class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }

    componentDidCatch(error, errorInfo) {
        console.error("ErrorBoundary caught an error:", error, errorInfo);
    }

    handleReload = () => {
        window.location.reload();
    };

    render() {
        if (this.state.hasError) {
            return (
                <div style={{ maxWidth: "600px", margin: "4rem auto", padding: "2rem", textAlign: "center" }}>
                    <h1>500 - Eroare neașteptată</h1>
                    <p style={{ color: "#666", margin: "1rem 0" }}>
                        A apărut o problemă în afișarea paginii. Vă rugăm reîncărcați sau încercați din nou.
                    </p>
                    <button type="button" onClick={this.handleReload} style={{ marginTop: "1rem" }}>
                        Reîncarcă pagina
                    </button>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;
