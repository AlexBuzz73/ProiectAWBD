function CategoryCard({ category, onDelete }) {
    const isSystem = category.isSystem === "Y";

    return (
        <div className="category-card">
            <div>
                <h3>{category.name}</h3>
                <p>
                    <span className={`badge ${isSystem ? "badge-admin" : "badge-user"}`}>
                        {isSystem ? "Sistem" : "Personalizată"}
                    </span>
                </p>
            </div>
            {!isSystem && (
                <button
                    type="button"
                    className="btn-danger btn-sm"
                    onClick={() => onDelete(category.categoryId)}
                >
                    Șterge
                </button>
            )}
        </div>
    );
}

export default CategoryCard;
