import CategoryCard from "./CategoryCard";

function CategoryList({ categories, onDelete }) {
    if (categories.length === 0) {
        return <p className="empty-state">Nicio categorie găsită.</p>;
    }

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-sm)" }}>
            {categories.map((category) => (
                <CategoryCard key={category.categoryId} category={category} onDelete={onDelete} />
            ))}
        </div>
    );
}

export default CategoryList;
