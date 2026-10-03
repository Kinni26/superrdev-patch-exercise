export default function SearchBar({ value, onChange }) {
  return (
    <input
      type="text"
      aria-label="Search tasks"
      className="search-input"
      placeholder="Search tasks..."
      value={value}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}
