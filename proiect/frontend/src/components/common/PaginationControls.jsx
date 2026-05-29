function PaginationControls({pageNumber, totalPages, first, last,
                             pageSize, pageSizeOptions, onPageChange, onPageSizeChange,
                            }) {
    if (totalPages === 0) {
        return null;
    }

    const getVisiblePages = () => {
        if (totalPages <= 5) {
            return Array.from({ length: totalPages }, (_, index) => index);
        }

        const pages = [0];

        const start = Math.max(1, pageNumber - 1);
        const end = Math.min(totalPages - 2, pageNumber + 1);

        if (start > 1) {
            pages.push("left-ellipsis");
        }

        for (let index = start; index <= end; index++) {
            pages.push(index);
        }

        if (end < totalPages - 2) {
            pages.push("right-ellipsis");
        }

        pages.push(totalPages - 1);

        return pages;
    };

    return (
        <div>
            <div>
                <button
                    type="button"
                    disabled={first}
                    onClick={() => onPageChange(pageNumber - 1)}
                >
                    Previous
                </button>

                {getVisiblePages().map((page) => {
                    if (page === "left-ellipsis" || page === "right-ellipsis") {
                        return (
                            <span key={page}>
                                {" "}
                                ...{" "}
                            </span>
                        );
                    }

                    return (
                        <button
                            key={page}
                            type="button"
                            disabled={page === pageNumber}
                            onClick={() => onPageChange(page)}
                        >
                            {page + 1}
                        </button>
                    );
                })}

                <button
                    type="button"
                    disabled={last}
                    onClick={() => onPageChange(pageNumber + 1)}
                >
                    Next
                </button>
            </div>

            <p>
                Page {pageNumber + 1} of {totalPages}
            </p>

            <div>
                <label>Items per page: </label>

                <select
                    value={pageSize}
                    onChange={(event) => onPageSizeChange(Number(event.target.value))}
                >
                    {pageSizeOptions.map((option) => (
                        <option key={option} value={option}>
                            {option}
                        </option>
                    ))}
                </select>
            </div>
        </div>
    );
}

export default PaginationControls;