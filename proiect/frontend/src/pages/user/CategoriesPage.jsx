import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getLoggedUser } from "../../utils/authStorage";
import { createCategory, deleteCategory, getCategoriesPaged, } from "../../api/categoriesApi";
import CategoryList from "../../components/categories/CategoryList";
import PaginationControls from "../../components/common/PaginationControls";

const EMPTY_CATEGORIES_PAGE = {
    content: [],
    pageNumber: 0,
    pageSize: 3,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
};

function CategoriesPage() {
    const user = getLoggedUser();
    const [categoriesPage, setCategoriesPage] = useState(EMPTY_CATEGORIES_PAGE);
    const [loadingCategories, setLoadingCategories] = useState(false);
    const [categoriesError, setCategoriesError] = useState("");
    const [categoryName, setCategoryName] = useState("");
    const [createError, setCreateError] = useState("");
    const [message, setMessage] = useState("");
    const [page, setPage] = useState(0);
    const [pageSize, setPageSize] = useState(3);
    const [sortBy, setSortBy] = useState("name");
    const [direction, setDirection] = useState("asc");

    const loadCategories = async () => {
        if (!user?.userId) {
            return;
        }

        setLoadingCategories(true);
        setCategoriesError("");

        try {
            const response = await getCategoriesPaged(user.userId, page, pageSize, sortBy, direction);

            setCategoriesPage(response);
        } catch (err) {
            setCategoriesError(err.message);
        } finally {
            setLoadingCategories(false);
        }
    };

    useEffect(() => {
        loadCategories();
    }, [user?.userId, page, pageSize, sortBy, direction]);

    const handleSortByChange = (event) => {
        setSortBy(event.target.value);
        setPage(0);
    };

    const handleDirectionChange = (event) => {
        setDirection(event.target.value);
        setPage(0);
    };

    const handlePageSizeChange = (newPageSize) => {
        setPageSize(newPageSize);
        setPage(0);
    };

    const handleCreateCategory = async (event) => {
        event.preventDefault();

        setCreateError("");
        setMessage("");

        if (!categoryName.trim()) {
            setCreateError("Category name is required.");
            return;
        }

        try {
            await createCategory(user.userId, {
                name: categoryName,
            });

            setCategoryName("");
            setMessage("Category created successfully.");
            setPage(0);
            loadCategories();
        } catch (err) {
            setCreateError(err.message);
        }
    };

    const handleDeleteCategory = async (categoryId) => {
        const confirmed = window.confirm("Are you sure you want to delete this category?");

        if (!confirmed) {
            return;
        }

        setCategoriesError("");
        setMessage("");

        try {
            await deleteCategory(user.userId, categoryId);

            setMessage("Category deleted successfully.");
            setPage(0);
            loadCategories();
        } catch (err) {
            setCategoriesError(err.message);
        }
    };

    if (!user) {
        return (
            <p>
                User is not logged in.
            </p>
        );
    }

    return (
        <div>
            <h1>Categories</h1>

            <Link to="/dashboard">
                Back to Dashboard
            </Link>

            <hr />

            {message && (
                <p style={{ color: "green" }}>
                    {message}
                </p>
            )}

            <h2>Create Category</h2>

            <form onSubmit={handleCreateCategory}>
                <div>
                    <label>Category name: </label>

                    <input
                        type="text"
                        value={categoryName}
                        onChange={(event) => setCategoryName(event.target.value)}
                    />
                </div>

                {createError && (
                    <p style={{ color: "red" }}>
                        {createError}
                    </p>
                )}

                <button type="submit">
                    Create Category
                </button>
            </form>

            <hr />

            <h2>Available Categories</h2>

            <div>
                <label>Sort by: </label>

                <select value={sortBy} onChange={handleSortByChange}>
                    <option value="name">Name</option>
                    <option value="usageCount">Usage Count</option>
                </select>

                <label> Direction: </label>

                <select value={direction} onChange={handleDirectionChange}>
                    <option value="asc">Ascending</option>
                    <option value="desc">Descending</option>
                </select>
            </div>

            {loadingCategories && <p>Loading categories...</p>}

            {categoriesError && (
                <p style={{ color: "red" }}>
                    {categoriesError}
                </p>
            )}

            {!loadingCategories && !categoriesError && (
                <>
                    <CategoryList
                        categories={categoriesPage.content}
                        onDelete={handleDeleteCategory}
                    />

                    {categoriesPage.totalElements > 0 && (
                        <PaginationControls
                            pageNumber={categoriesPage.pageNumber}
                            totalPages={categoriesPage.totalPages}
                            first={categoriesPage.first}
                            last={categoriesPage.last}
                            pageSize={pageSize}
                            pageSizeOptions={[3, 5]}
                            onPageChange={setPage}
                            onPageSizeChange={handlePageSizeChange}
                        />
                    )}
                </>
            )}
        </div>
    );
}

export default CategoriesPage;