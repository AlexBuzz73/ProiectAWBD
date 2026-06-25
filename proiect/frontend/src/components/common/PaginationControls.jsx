function PaginationControls({ pageNumber, totalPages, first, last,
                              pageSize, pageSizeOptions, onPageChange, onPageSizeChange }) {
    if (totalPages === 0) return null;

    const getVisiblePages = () => {
        if (totalPages <= 5) return Array.from({ length: totalPages }, (_, i) => i);
        const pages = [0];
        const start = Math.max(1, pageNumber - 1);
        const end = Math.min(totalPages - 2, pageNumber + 1);
        if (start > 1) pages.push("left-ellipsis");
        for (let i = start; i <= end; i++) pages.push(i);
        if (end < totalPages - 2) pages.push("right-ellipsis");
        pages.push(totalPages - 1);
        return pages;
    };

    return (
        <div className="pagination">
            <div className="pagination-controls">
                <button type="button" disabled={first} onClick={() => onPageChange(pageNumber - 1)}>← Înapoi</button>
                {getVisiblePages().map((page) => {
                    if (page === "left-ellipsis" || page === "right-ellipsis") {
                        return <span key={page}>…</span>;
                    }
                    return (
                        <button
                            key={page}
                            type="button"
                            disabled={page === pageNumber}
                            onClick={() => onPageChange(page)}
                            style={page === pageNumber ? { background: "var(--accent)", color: "#fff", borderColor: "var(--accent)" } : {}}
                        >
                            {page + 1}
                        </button>
                    );
                })}
                <button type="button" disabled={last} onClick={() => onPageChange(pageNumber + 1)}>Înainte →</button>
            </div>

            <div style={{ display: "flex", alignItems: "center", gap: "var(--space-lg)" }}>
                <span className="pagination-info">Pagina {pageNumber + 1} din {totalPages}</span>
                {pageSizeOptions && (
                    <div className="pagination-size">
                        <label>Per pagină:</label>
                        <select value={pageSize} onChange={(e) => onPageSizeChange(Number(e.target.value))}>
                            {pageSizeOptions.map((opt) => <option key={opt} value={opt}>{opt}</option>)}
                        </select>
                    </div>
                )}
            </div>
        </div>
    );
}

export default PaginationControls;
