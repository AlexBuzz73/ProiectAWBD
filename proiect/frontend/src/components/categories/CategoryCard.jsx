function CategoryCard({ category, onDelete }) {
    const isSystemCategory = category.isSystem === "Y";

    return (
        <div>
            <h3>{category.name}</h3>

            <p>
                <strong>Type:</strong> {isSystemCategory ? "System" : "Custom"}
            </p>

            <p>
                <strong>Status:</strong> {category.status}
            </p>

            {!isSystemCategory && (
                <button
                    type="button"
                    onClick={() => onDelete(category.categoryId)}
                >
                    Delete
                </button>
            )}

            <hr />
        </div>
    );
}

export default CategoryCard;