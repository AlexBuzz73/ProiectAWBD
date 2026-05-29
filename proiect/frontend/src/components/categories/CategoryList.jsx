import CategoryCard from "./CategoryCard";

function CategoryList({ categories, onDelete }) {
    if (categories.length === 0) {
        return (
            <p>
                No categories found.
            </p>
        );
    }

    return (
        <div>
            {categories.map((category) => (
                <CategoryCard
                    key={category.categoryId}
                    category={category}
                    onDelete={onDelete}
                />
            ))}
        </div>
    );
}

export default CategoryList;