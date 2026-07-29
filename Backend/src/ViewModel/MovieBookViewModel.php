<?php
namespace App\ViewModel;

use App\Model\MovieBook;

/** 影音书籍视图模型。 */
class MovieBookViewModel {
    public static function fromArray(array $a): MovieBook { return MovieBook::fromUploadArray($a); }
    public static function toArray(MovieBook $m): array { return $m->toApiArray(); }
    public static function listToArray(array $list): array {
        return array_map(static fn(MovieBook $m) => $m->toApiArray(), $list);
    }
}
